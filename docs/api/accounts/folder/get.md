# Get Account Details

Show a single Account details.

**URL** : `/api/accounts/:number`

**URL Parameters** : `number=[string]` where `number` is IBAN of the Account.

**Method** : `GET`

**Permissions required** : None

## Success Response

**Condition** : If Account exists

**Code** : `200 OK`

**Content example**

```json
{
    "balance": {
        "amount": "0.00",
        "currency": "EUR"
    },
    "closedAt": "",
    "currency": "EUR",
    "holder": {
        "address": "Somewhere, Belgium",
        "fullName": "Maxim Korneev"
    },
    "number": "BE92952028423353",
    "openedAt": "2019-02-13"
}
```

## Error Responses

**Condition** : If Account does not exist with provided `number` parameter.

**Code** : `404 NOT FOUND`

**Content example**

```json
{
    "code": 404,
    "message": "Account BE92952028423353 not found"
}
```