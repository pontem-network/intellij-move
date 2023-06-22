<fold text='/* ... */'>/// my docstring
/// my other docstring</fold>
module 0x1::M <fold text='{...}'>{
    const <fold text='...'> MY_CONST: u64 = 1;
    const MY_CONST_2: u64 = 1</fold>;

    <fold text='/* ... */'>/// my docstring
    /// my other docstring</fold>
    fun main() <fold text='/* acquires */'>acquires A, B, C</fold> <fold text='{...}'>{
        let a = 1;
    }</fold>

    spec main <fold text='{...}'>{
        assert true;
    }</fold>

    spec fun spec_fun <fold text='{...}'>{
        assert true;
    }</fold>

    spec module {}

    <fold text='/* ... */'>/// my docstring
    /// my other docstring</fold>
    struct R <fold text='{...}'>{ val: u8 }</fold>
    }</fold>
