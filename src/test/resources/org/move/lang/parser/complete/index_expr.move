module 0x1::index_expr {
    struct X<M> has copy, drop, store {
        value: M
    }

    struct Y<T> has key, drop {
        field: T
    }

    fun test_vector() {
        let v = vector[x, x];
        assert!(v[0].value == 2, 0);
    }

    fun test_vector_borrow_mut() {
        let v = vector[y1, y2];
        (&mut v[0]).field.value = false;
        (&mut v[1]).field.value = true;
        assert!((&v[0]).field.value == false, 0);
        assert!((&v[1]).field.value == true, 0);
    }

    fun test_resource_3() acquires R {
        use 0x42::test;
        assert!((&test::Y<X<bool>>[@0x1]).field.value == true, 0);
    }

    fun test_resource_4() acquires R {
        let addr = @0x1;
        let y = &mut 0x42::test::Y<X<bool>> [addr];
        y.field.value = false;
        spec {
            assert Y<X<bool>>[addr].field.value == false;
        };
        assert!((&Y<X<bool>>[addr]).field.value == false, 1);
    }

    fun test_resource_5() acquires Y {
        let addr = @0x1;
        let y = &mut 0x42::test::Y<X<bool>> [addr];
        y.field.value = false;
        let y_resource = Y<X<bool>>[addr];
        assert!(y_resource.field.value == false, 1);
    }
}
