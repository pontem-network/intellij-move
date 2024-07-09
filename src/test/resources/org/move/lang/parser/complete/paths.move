module 0x1::paths {
    fun main() {
        0x1;
        0x1::m;
        std::m;

        0x1::m::call;
        std::m::call;

        0x1::m::Enum::EnumType;
        std::m::Enum::EnumType;

        std<>;
        std<>::item;
        std::m<>;
        std::m<>::item;
        std::m::call<>;
        std::m::call<>::item;
        std::m::Enum::EnumType<>;
        std::m::Enum::EnumType<>::item;
    }
}
