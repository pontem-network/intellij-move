module 0x1::public_package {

    public(package) fun package(package: u8): u8 {
        let package = package + 1;
        package
    }
    package fun my_package_fun(): u8 {}
}
