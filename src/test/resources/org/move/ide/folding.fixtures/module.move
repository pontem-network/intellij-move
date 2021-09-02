module M <fold text='{...}'>{
    fun main() <fold text='{...}'>{
        let a = 1;
    }</fold>

    spec main <fold text='{...}'>{
        assert true;
    }</fold>

    spec fun spec_fun <fold text='{...}'>{
        assert true;
    }</fold>

    resource struct R <fold text='{...}'>{ val: u8 }</fold>
}</fold>
