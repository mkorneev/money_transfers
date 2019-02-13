# Create Account

Create an Account for the authenticated User if an Account for that User does
not already exist. Each User can only have one Account.

**URL** : `/api/accounts/`

**Method** : `POST`

**Permissions required** : None

**Data constraints**

Provide currency and holder details of Account to be created.

```json
{
    "holder": {
      "fullName": "[unicode]",
      "address": "[unicode]"
    },
    "currency": "[3 chars code]"    
}
```

**Data example** All fields must be sent.

```json
{
    "holder": {
      "fullName": "Maxim Korneev",
      "address": "Somewhere, Belgium"
    },
    "currency": "EUR"
}
```

## Success Response

**Condition** : If everything is OK

**Code** : `201 CREATED`

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

**Condition** : If fields are missing.

**Code** : `400 BAD REQUEST`

**Content example**

```json
{
    "code": 400,
    "message": "Required property 'address' missing at $.holder"
}
```