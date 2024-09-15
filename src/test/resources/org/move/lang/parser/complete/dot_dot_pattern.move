module 0x1::dot_dot_pattern {
    fun main() {
        let S2(..) = x;
        let S2(_x, ..) = x;
        let S2(.., _x) = x;
        let S2(.., _) = x;
        let S2(_, ..) = x;
        let S2(_x, _y, ..) = x;
        let S2(_x, .., _y) = x;
        let S2(.., _x, _y) = x;

        let S3 { .. } = x;
        let S3 { x: _x, .. } = x;
        let S3 { y: _y, .. } = x;

        let S4 { x: _x, .. } = x;
        let S4 { y: _y, .. } = x;
        let S4 { y: S3 { .. }, .. } = x;
        let S4 { y: S3 { x: _x, .. }, .. } = x;
        let S4 { y: S3 { x: _x1, .. }, x: _x2 } = x;
        let S4 { y: S3 { y: _y, .. }, .. } = x;
        let S4 { y: S3 { x: _x1, .. }, x: _x2 } = x;

        let S5(.., S1(..)) = x;
        let S5(.., S1(..)) = x;
        let S5(.., S4 { .. }) = x;
        let S4 { x: S1(..), .. } = x;
    }
}
