module 0x1::chop_wraps {
    fun long_arguments(addr1: address, addr1: address, addr1: address, addr1: address, addr1: address, addr1: address) {}

    struct EventsStore<phantom X, phantom Y, phantom Curve> has key { storage_registered_handle: event::EventHandle<StorageCreatedEvent<X, Y, Curve>>, coin_deposited_handle: event::EventHandle<CoinDepositedEvent<X, Y, Curve>>, coin_withdrawn_handle: event::EventHandle<CoinWithdrawnEvent<X, Y, Curve>>,
    }

    #[test(account = @my_address, account = @my_address, account = @my_address, account = @my_address, account = @my_address, account = @my_address)]
    fun main() {
        call(1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5, 1 + 2 + 3 + 4 + 5)
    }
}
