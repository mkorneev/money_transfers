# Withdraw Money

Withdraw money from an Account.

**URL** : `/api/accounts/:number/withdraw`

**URL Parameters** : `number=[string]` where `number` is IBAN of the Account.

**Method** : `POST`

**Permissions required** : None

**Data constraints**

Provide withdraw request details.

```json
{
    "accountNumber": "[account_number]",
    "amount": {
        "amount": "[decimal number]",
        "currency": "[3 chars currency code]"
    },
    "message": "[unicode string]"
}
```

**Data example** All fields must be sent except `message`.

```json
{
    "accountNumber": "BE59051227391008",
    "amount": {
        "amount": "50.00",
        "currency": "EUR"
    }
}
```

## Success Response

**Condition** : If everything is OK

**Code** : `200 OK`

**Content example**

```json
{
    "balance": {
        "amount": "10.00",
        "currency": "EUR"
    },
    "closedAt": "",
    "currency": "EUR",
    "holder": {
        "address": "Somewhere, Belgium",
        "fullName": "Maxim Korneev"
    },
    "number": "BE59051227391008",
    "openedAt": "2019-02-13"
}
```

## Error Responses

TBD