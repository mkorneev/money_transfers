RESTful API for money transfers between accounts.

## Open Endpoints

Open endpoints require no Authentication.

### Account related

Endpoints for opening accounts and transfering money.

* [Open An Account](accounts/post.md) : `POST /api/accounts/`
* [Get Account Details](accounts/number/get.md) : `GET /api/accounts/:number`
* [Get Account Balance](accounts/number/balance/get.md) : `GET /api/accounts/:number/balance`

* [Deposit Money](accounts/number/deposit/post.md) : `POST /api/accounts/:number/deposit`
* [Withdraw Money](accounts/number/withdraw/post.md) : `POST /api/accounts/:number/withdraw`
* [Transfer Money](transfer/post.md) : `POST /api/transfer`
