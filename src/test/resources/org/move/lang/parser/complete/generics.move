script {
    fun main<T, U>() {
        let a = vector<u8>;
        let b = map<u8, u8>;
        let c = vector<vector<u8>>;
        let c = map<u8, vector<u8>>;

        vector<u8> == vector<u8>;
        vector<u8> >= vector<u8>;
    }
}