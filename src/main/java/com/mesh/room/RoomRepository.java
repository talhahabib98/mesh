package com.mesh.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByName(String name);

    boolean existsByName(String name);
}
