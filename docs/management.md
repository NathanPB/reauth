The Management API is a feature designed to allow full control over the ReAuth server. This should only be used in highly trusted and secure environments, because this can expose your entire system to attackers if something is done wrong.

**Be Careful.**

# Manager Accounts

The management accounts can be found in the ``management.json`` file. This file consists in an array of JSONObjects with the following model:

```json
{
  "id": "reauth-manager-example",
  "displayName": "reAuth Example Manager",
  "token": "minimum 64 byte long string"
}
```

## Manager Account Model
| key         | description |
| ----------- | ----------- |
| id          | Account ID. Only lowercase letters, numbers and ``-`` are acceptable. Must not duplicate any existing manger account IDs. |
| displayName | Display name of the account. Cannot be blank |
| token       | Minimum 64 byte long random string. Must be random and stored safely |

# Management GraphQL API

A GraphQL API is exposed to do management actions under ``BASE_URL/graphql``. Use schema introspection for more details.

You must include the header ``Authorization: Bearer YOUR_TOKEN`` in every request made to that API.
