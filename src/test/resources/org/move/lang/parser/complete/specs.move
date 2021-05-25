module BCS {
    spec module {
        native fun serialize<MoveValue>(v: &MoveValue): vector<u8>;
    }
}
