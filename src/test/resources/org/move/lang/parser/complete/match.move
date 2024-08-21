module 0x1::match {

    fun t1(self: Color): bool {
        match (self) {
            Color::Red => true,
            Color::Blue => false,
            Color::RGB{red, green, blue} => red + green + blue > 0,
        }
    }

    fun t1_field_named(self: Color): bool {
        match (self) {
            Color::Red => true,
            Color::Blue => false,
            Color::RGB{red: r, green: g, blue} => r + g + blue > 0,
        }
    }

    fun t1_uses_block(self: Color): bool {
        match (self) {
            Color::RGB{red, green, blue} => { red + green + blue > 0 },
            Color::Red => true,
            Color::Blue => false,
        }
    }

    fun t1_uses_block_no_comma(self: Color): bool {
        match (self) {
            Color::RGB{red, green, blue} => { red + green + blue > 0 }
            Color::Red => true,
            Color::Blue => false,
        }
    }

    fun t1_module_qualified(self: m::Color): bool {
        match (self) {
            m::Color::RGB{red, green, blue} => red + green + blue > 0,
            m::Color::Red => true,
            m::Color::Blue => false,
        }
    }

    fun t1_address_qualified(self: m::Color): bool {
        match (self) {
            0x815::m::Color::RGB{red, green, blue} => red + green + blue > 0,
            0x815::m::Color::Red => true,
            Color::Blue => false,
        }
    }

    fun t2(self: Color): bool {
        match (self) {
            Color::RGB{red, green, blue} => red + green + blue > 0,
            _ => true,
        }
    }

    fun t3(self: Color): bool {
        match (&self) {
            Color::RGB{red, green, blue} => *red + *green + *blue > 0,
            _ => true,
        }
    }

    fun t4(self: Color): Color {
        match (&mut self) {
            Color::RGB{red, green: _, blue: _} => *red = 2,
            _ => {},
        };
        self
    }

    fun t5_freeze(self: Color): u64 {
        let x = 1;
        let r = match (&mut self) {
            Color::Red => &x,
            Color::Blue => &mut x,
            _ => &mut x,
        };
        *r
    }

    fun t6_construct(self: Color): Color {
        match (self) {
            Color::RGB{red, green, blue} => Color::RGB{red: red + 1, green: green - 1, blue},
            _ => self
        }
    }

    fun t8_unqualified_variant(self: Color): bool {
        match (self) {
            RGB{red, green, blue} => red != green && green != blue,
            Red => true,
            Blue => false,
            One{i: _} => {}
            _ => {}
            One{i} if consume(i) => Outer::One{i},
            o => o,
            Some{value: Option::None} => false,
        }
    }

    fun match_as_function() {
        match(1);
    }
}
