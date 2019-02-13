# Transfer Money

Transfer money between accounts.

**URL** : `/api/transfer`

**Method** : `POST`

**Permissions required** : None

**Data constraints**

Provide transfer details.

```json
{
    "from": "[account_number]",
    "to": "[account_number]",
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
    "from": "BE59051227391008",
    "to": "BE92952028423353",
    "amount": {
        "amount": "0.01",
        "currency": "EUR"
    },
    "message": "1 cent"
}
```

## Success Response

**Condition** : If everything is OK

**Code** : `201 CREATED`

**Content example**

```json
{
    "amount": {
        "amount": "0.01",
        "currency": "EUR"
    },
    "from": "BE59051227391008",
    "id": "c32a8c31-087e-4c6b-afec-d187bb93b274",
    "message": "1 cent",
    "timestamp": 1550085640771,
    "to": "BE92952028423353"
}
```

## Error Responses

**Condition** : If one of the accounts is not found.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "code": 400,
    "message": "Account BE59051227391008 not found"
}
```

TBD