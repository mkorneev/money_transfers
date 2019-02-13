RESTful API for money transfers between accounts.

## Open Endpoints

Open endpoints require no Authentication.

### Account related

Endpoints for opening accounts and transfering money.

* [Open An Account](accounts/post.md) : `POST /api/accounts/`
* [Get Account Details](accounts/number/get.md) : `GET /api/accounts/:number`
* [Get Account Balance](accounts/number/balance.md) : `GET /api/accounts/:number/balance`

* [Deposit Money](accounts/number/deposit.md) : `GET /api/accounts/:number/deposit`
* [Withdraw Money](accounts/number/withdraw.md) : `GET /api/accounts/:number/withdraw`
* [Transfer Money](transfer.md) : `GET /api/transfer`
