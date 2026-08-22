# Changelog

## [0.2.0](https://github.com/FGBxRamel/TesseraniaEconomySystem/compare/v0.1.0...v0.2.0) (2026-08-22)


### Features

* **shop:** add /tes shop liste with pagination and teleport ([699ac32](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/699ac328ab7f81dab7692492787c988f30825c3b))
* **shop:** add chat-driven creation/edit/close flow ([dd0e6b1](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/dd0e6b1e1037e7f0cc36a0cf49e939cf499d7d50))
* **shop:** add container conversion and protection ([5298bc9](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/5298bc926835da01c5bb30429a32a5705bdf9ecc))
* **shop:** add purchase, refund, withdraw and restock (UC4) ([aad2473](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/aad2473034157cfc14ca6bea3bcb8aba3aeffc0f))
* **shop:** add sell-all-items shop type ([81ebeab](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/81ebeab7e24c1ea5d693ba12b1d5120492aad716))
* **shop:** add shop/transaction schema and TP/EP admin commands ([cb06281](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/cb06281278fbb525a14e4b6d7eebc4a7ad0aa71a))
* **shop:** wire transaction completion, buyer TP/EP accrual, and orphan cleanup ([aa7a4d5](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/aa7a4d5bbaeabd21d9aa3dc06f310b7035224d1d))
* Stage 1 — Item Shops & Transaction Capture ([4449fc4](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/4449fc472de3d653cca3665e6c1b44bc069ffd76))


### Bug Fixes

* **run-config:** use MavenRunnerParameters schema for Run Test Server ([90c2f08](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/90c2f08b5bb1bc3aa9f0f2054954960537604213))
* **shop:** cap item shop price at 64 diamonds per slot ([d3db6e7](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/d3db6e7bfb9d0f8645c43f6657bfe1c415e71233))
* **shop:** correct chest title, block protection, and purchase cooldowns ([ac62ce6](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/ac62ce6ca894c7f9c19bb4b519424ec8298c284c))
* **shop:** hard-delete shops on close so IDs become reusable ([aab3c02](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/aab3c029eac21c3cf1e08c8437cd12054e8f0b0d))
* **shop:** preserve item metadata through shop purchase/restock/refund ([33e4aa2](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/33e4aa2d5360db1539e371cbcbd88142f3722d5b))
* **shop:** remove purchased item from buyer on refund ([66ba19a](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/66ba19a011fae127dc0b444941e9ef078d075d3d))
* Shops can no longer be destroyed in survival ([554beaf](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/554beafb11ed2965eb861c4d1a2bfd6ddbcc34e5))
* **shop:** strip refund-window cooldown tag before diamond withdrawal ([8cc47aa](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/8cc47aa35625b37159dd1ca018ce0983eff425e4))

## 0.1.0 (2026-08-18)


### Features

* add one-click local test server via run-paper-maven-plugin ([31a50bf](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/31a50bf3a0c9d9bd2821b5d73792c39ed0d69408))
* auto-install LuckPerms on the local dev/test server ([3ea6c49](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/3ea6c496f69338c2e78e496dcb19d511d8f831f7))
* implement Stage 0 foundation and release CI ([826c7ca](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/826c7ca936b34da79ecd1471030915dfbeeb088b))
* implement Stage 0 foundation and release CI ([f0a3712](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/f0a3712ac3b05b49663d7dd9db229bf1af9f0d9f))


### Bug Fixes

* **ci:** force first release-please release to 0.1.0 ([4aca6cd](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/4aca6cd517d70918086d901369b2b825393df009))
* **ci:** force the first release-please release to 0.1.0 ([6dd731d](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/6dd731d24a939f8f36c6a0fb14d5acb2c1680117))
* **ci:** use a PAT for release-please ([ed77090](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/ed77090eac1228a2c2b5c90ac5eaf5e6ac7bea9e))
* **ci:** use a PAT for release-please, GITHUB_TOKEN can't open PRs ([a9e5fb9](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/a9e5fb94c88b1b05cd24de416448c0b31da35edc))
* **ci:** use actions/setup-java@v5 ([b76da1a](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/b76da1ad84e211db429cd1e69880ca67cf0f1728))
* **command:** register /tes via JavaPlugin#registerCommand ([7536043](https://github.com/FGBxRamel/TesseraniaEconomySystem/commit/7536043bf72db02c4cd88c0b0aceea22448693e1))
