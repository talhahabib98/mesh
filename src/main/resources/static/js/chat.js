(() => {
    const state = {
        token: null,
        username: null,
        currentRoomId: null,
        stompClient: null,
    };

    const authScreen = document.getElementById("auth-screen");
    const chatScreen = document.getElementById("chat-screen");
    const authError = document.getElementById("auth-error");
    const currentUsernameEl = document.getElementById("current-username");
    const roomListEl = document.getElementById("room-list");
    const presenceListEl = document.getElementById("presence-list");
    const roomHeaderEl = document.getElementById("room-header");
    const messagesEl = document.getElementById("messages");
    const sendFormEl = document.getElementById("send-form");
    const messageInputEl = document.getElementById("message-input");

    async function api(path, options = {}) {
        const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
        if (state.token) {
            headers["Authorization"] = "Bearer " + state.token;
        }

        const response = await fetch(path, { ...options, headers });

        if (!response.ok) {
            let message = "Request failed (" + response.status + ")";
            try {
                const body = await response.json();
                message = body.detail || body.message || message;
            } catch (ignored) {
                // response had no JSON body
            }
            throw new Error(message);
        }

        if (response.status === 204) {
            return null;
        }
        return response.json();
    }

    function showChatScreen() {
        authScreen.classList.add("hidden");
        chatScreen.classList.remove("hidden");
        currentUsernameEl.textContent = state.username;
    }

    function showAuthScreen() {
        chatScreen.classList.add("hidden");
        authScreen.classList.remove("hidden");
    }

    async function afterAuth(auth) {
        state.token = auth.token;
        state.username = auth.username;
        showChatScreen();
        connectStomp();
        await loadRooms();
    }

    document.getElementById("login-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        authError.textContent = "";
        try {
            const auth = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    username: document.getElementById("login-username").value,
                    password: document.getElementById("login-password").value,
                }),
            });
            await afterAuth(auth);
        } catch (err) {
            authError.textContent = err.message;
        }
    });

    document.getElementById("register-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        authError.textContent = "";
        try {
            const auth = await api("/api/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    username: document.getElementById("register-username").value,
                    email: document.getElementById("register-email").value,
                    password: document.getElementById("register-password").value,
                }),
            });
            await afterAuth(auth);
        } catch (err) {
            authError.textContent = err.message;
        }
    });

    document.getElementById("logout-button").addEventListener("click", () => {
        if (state.stompClient) {
            state.stompClient.deactivate();
        }
        state.token = null;
        state.username = null;
        state.currentRoomId = null;
        showAuthScreen();
    });

    document.getElementById("create-room-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const input = document.getElementById("new-room-name");
        try {
            await api("/api/rooms", { method: "POST", body: JSON.stringify({ name: input.value }) });
            input.value = "";
            await loadRooms();
        } catch (err) {
            alert(err.message);
        }
    });

    sendFormEl.addEventListener("submit", (event) => {
        event.preventDefault();
        const content = messageInputEl.value.trim();
        if (!content || !state.currentRoomId || !state.stompClient || !state.stompClient.connected) {
            return;
        }
        state.stompClient.publish({
            destination: "/app/chat.send",
            body: JSON.stringify({ roomId: state.currentRoomId, content }),
        });
        messageInputEl.value = "";
    });

    async function loadRooms() {
        const rooms = await api("/api/rooms");
        roomListEl.innerHTML = "";
        rooms.forEach((room) => {
            const li = document.createElement("li");
            li.textContent = room.name;
            li.dataset.roomId = room.id;
            if (room.id === state.currentRoomId) {
                li.classList.add("active");
            }
            li.addEventListener("click", () => selectRoom(room));
            roomListEl.appendChild(li);
        });
    }

    async function selectRoom(room) {
        state.currentRoomId = room.id;
        roomHeaderEl.textContent = "# " + room.name;
        messagesEl.innerHTML = "";
        sendFormEl.classList.remove("hidden");

        Array.from(roomListEl.children).forEach((li) => {
            li.classList.toggle("active", li.dataset.roomId === room.id);
        });

        const history = await api("/api/rooms/" + room.id + "/messages");
        history.forEach(renderMessage);

        const onlineUsers = await api("/api/rooms/" + room.id + "/presence");
        renderPresenceList(onlineUsers);

        subscribeToRoom(room.id);
    }

    function renderMessage(message, originNode) {
        const el = document.createElement("div");
        el.classList.add("message");
        if (message.senderUsername === state.username) {
            el.classList.add("mine");
        }

        const meta = document.createElement("div");
        meta.classList.add("meta");
        meta.textContent = message.senderUsername + (originNode ? " · via " + originNode : "");

        const body = document.createElement("div");
        body.textContent = message.content;

        el.appendChild(meta);
        el.appendChild(body);
        messagesEl.appendChild(el);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function renderSystemLine(text) {
        const el = document.createElement("div");
        el.classList.add("message", "system");
        el.textContent = text;
        messagesEl.appendChild(el);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function renderPresenceList(usernames) {
        presenceListEl.innerHTML = "";
        usernames.forEach((username) => {
            const li = document.createElement("li");
            li.textContent = username;
            presenceListEl.appendChild(li);
        });
    }

    let roomSubscriptions = [];

    function subscribeToRoom(roomId) {
        roomSubscriptions.forEach((sub) => sub.unsubscribe());
        roomSubscriptions = [];

        if (!state.stompClient || !state.stompClient.connected) {
            return;
        }

        roomSubscriptions.push(
            state.stompClient.subscribe("/topic/rooms/" + roomId, (frame) => {
                const payload = JSON.parse(frame.body);
                renderMessage(payload.message, payload.originNode);
            })
        );

        roomSubscriptions.push(
            state.stompClient.subscribe("/topic/rooms/" + roomId + "/presence", (frame) => {
                const event = JSON.parse(frame.body);
                renderSystemLine(event.username + " " + event.status.toLowerCase() + " the room");
                api("/api/rooms/" + roomId + "/presence").then(renderPresenceList);
            })
        );
    }

    function connectStomp() {
        const client = new StompJs.Client({
            webSocketFactory: () => new SockJS("/ws"),
            connectHeaders: { Authorization: "Bearer " + state.token },
            reconnectDelay: 3000,
        });

        client.onConnect = () => {
            if (state.currentRoomId) {
                subscribeToRoom(state.currentRoomId);
            }
        };

        client.activate();
        state.stompClient = client;
    }
})();
