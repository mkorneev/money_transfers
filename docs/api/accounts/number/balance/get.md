# Get Account Balance

Show a single Account balance.

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
    "amount": "0.00",
    "currency": "EUR"
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