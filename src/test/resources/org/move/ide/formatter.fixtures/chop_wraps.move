module 0x1::chop_wraps {
    use 0x1::m::{long_function_1, long_function_2};
    use 0x1::m::{long_function_1, long_function_2, long_function_3, long_function_4, long_function_5, long_function_6, long_function_7};

    fun long_arguments(addr1: address, addr1: address, addr1: address, addr1: address, addr1: address, addr1: address) {}

    struct EventsStore<phantom X, phantom Y, phantom Curve> has key { storage_registered_handle: event::EventHandle<StorageCreatedEvent<X, Y, Curve>>, coin_deposited_handle: event::EventHandle<CoinDepositedEvent<X, Y, Curve>>, coin_withdrawn_handle: event::EventHandle<CoinWithdrawnEvent<X, Y, Curve>>,
    }

    #[test(account = @my_address, account = @my_address, account = @my_address, account = @my_address, account = @my_address, account = @my_address)]
    fun main() {
        call(1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5)
    }
}
