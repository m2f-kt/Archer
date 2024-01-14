package com.m2f.archer.crud.cache

//class CacheExpirationTest : FunSpec({
//
//    //"passing expiration"
//
//    test("never expires") {
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
//        val neverExpires = store.expires(Never)
//        neverExpires.get(0) shouldBeRight "Test"
//    }
//
//    test("always expires") {
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
//        val alwaysExpires = store.expires(Always)
//        alwaysExpires.get(0) shouldBeLeft Invalid
//    }
//
//
//    //"expires with time"
//    test("fetching after time passed") {
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
//        val time = 50.milliseconds
//        val expiresAfter10Millis = store.expires(After(time), InMemoryDataSource())
//        expiresAfter10Millis.put(0, "test10")
//        delay(100L)
//        expiresAfter10Millis.get(0) shouldBeLeft Invalid
//    }
//
//    test("fetching before time passes") {
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
//        val time = 50.milliseconds
//        val expiresAfter10Millis = store.expires(After(time), InMemoryDataSource())
//        expiresAfter10Millis.put(0, "test10_2")
//        expiresAfter10Millis.get(0) shouldBeRight "test10_2"
//    }
//
//    //    "create an a caching strategy with expiration"
//
//    test("create a never expiring strategy") {
//        val main = getDataSource<Int, String> { "main" }
//
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
//        val cacheStrategyNever = main cacheWith store expires Never
//
//        //The cache never expires, so it should always return the value that we set in the default constructor
//        cacheStrategyNever.get(StoreSyncOperation, 0) shouldBeRight "Test from Store"
//    }
//
//    test("creating an always expiring strategy") {
//        val main = getDataSource<Int, String> { "main" }
//
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
//        val cacheStrategyAlways = main cacheWith store expires Always
//
//        //The cache always expires, so it should always return the value after storing it.
//        cacheStrategyAlways.get(StoreSyncOperation, 0) shouldBeRight "main from Store"
//    }
//
//    test("test expiration with a time expiring strategy") {
//        val main = getDataSource<Int, String> { "main" }
//
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
//        val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds
//
//        //Fetch the value from the main source and store it afterward
//        cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"
//
//        //Wait few milliseconds to let the cache expire
//        delay(100L)
//
//        //The cache is expired so if we enforce data from store should be invalid
//        cacheStrategyAfter.get(StoreOperation, 0) shouldBeLeft Invalid
//
//    }
//
//    test("test no-expiration with a time expiring strategy") {
//        val main = getDataSource<Int, String> { "main" }
//
//        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
//        val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds
//
//        //Fetch the value from the main source and store it afterward
//        cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"
//
//        //there's no delay so the cache did not expire
//        cacheStrategyAfter.get(StoreOperation, 0) shouldBeRight "main from Store"
//
//    }
//
//    // "expiration rules"
//
//    test("with a time expiration if there is no stored expiration date, the data then is expired") {
//        val main = getDataSource<Int, String> { "main" }
//
//        val store: StoreDataSource<Int, String> = InMemoryDataSource<Int, String>().map { "$it from Store" }
//
//        val cacheRegistry = MemoizedExpirationCache()
//        val cacheStrategyAfter = (main cacheWith store.expires(
//            After(24.hours),
//            cacheRegistry
//        )).build()
//
//        //get from main and store it
//        cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"
//
//        cacheRegistry.delete(
//            Delete(
//                CacheMetaInformation(
//                    key = 0.toString(),
//                    classIdentifier = String::class.simpleName.toString()
//                )
//            )
//        )
//
//        //as we don't store the expirations the data should be expired
//        cacheStrategyAfter.get(StoreOperation, 0) shouldBeLeft Invalid
//    }
//
//    test("If the data is empty should remove the expiration date") {
//
//        val info = CacheMetaInformation(
//            key = "0",
//            classIdentifier = String::class.simpleName.toString()
//        )
//
//        val expiration = mapOf(info to Clock.System.now() + 24.hours)
//        val expirationCache = InMemoryDataSource(expiration)
//
//        val store: StoreDataSource<Int, String> = InMemoryDataSource()
//
//        //the data does not exist
//        store.expires(After(24.hours), expirationCache)
//            .get(0) shouldBeLeft DataNotFound
//
//        //the stored expiration should be removed
//        expirationCache.get(info) shouldBeLeft DataNotFound
//    }
//})