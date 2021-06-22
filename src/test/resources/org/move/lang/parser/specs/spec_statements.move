module M {
    spec schema Emits<EventType> {
        emits msg to handle if !is_synthetic;
    }
}
