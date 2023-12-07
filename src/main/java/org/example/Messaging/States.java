package org.example.Messaging;

public enum States {
    HANDSHAKE_INITIATED,
    HANDSHAKE_COMPLETED,
    LIST_UPDATE_REQUESTED,
    LIST_UPDATE_COMPLETED,
    LIST_UPDATE_FAILED,

    RETRIEVE_LIST_REQUESTED,

    RETRIEVE_LIST_COMPLETED,

    RETRIEVE_LIST_FAILED,
    RETRIEVE_LISTS_REQUESTED,
    RETRIEVE_LISTS_COMPLETED,
    LIST_DELETE_REQUESTED,
    LIST_DELETE_COMPLETED,
    LIST_DELETE_FAILED,
    HASH_RING_UPDATE,
    HASH_RING_UPDATE_ACK,
    LIST_UPDATE_REQUESTED_MAIN,
    RETRIEVE_LIST_REQUESTED_MAIN,
    PING,
    PONG, NULL,
}
