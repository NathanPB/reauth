/*
 * Copyright (c) 2021 - Nathan P. Bombana
 *
 * This file is part of Reauth.
 *
 * Wheres My Duo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wheres My Duo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Wheres My Duo.  If not, see <https://www.gnu.org/licenses/>.
 */

const express = require('express')
const bodyParser = require('body-parser')
const Axios = require('axios')
const port = 3000

const app = express()
app.use(bodyParser.json())

const reauth = Axios.create({
  baseURL: 'http://localhost:6660/'
})

app.get('/', (req, res) => {
  res.sendFile('views/index.html', { root: __dirname })
})

app.get('/authorize', (req, res) => {
  res.sendFile('views/authorize.html', { root: __dirname })
})

app.get('/callback', async (req, res) => {
  const body = {
    "code": req.query.code,
    "grant_type": "authorization_code",
    "redirect_uri": "http://localhost:3000",
    "client_id": "123",
    "client_secret": "superman"
  }

  const token = await reauth.post('/oauth/token', body, {
    headers: { 'Content-Type': 'application/json' }
  })

  const user = await reauth.get('/identity', {
    headers: { Authorization: `${token.data.token_type} ${token.data.access_token}` }
  })

  res.send({
    token: token.data,
    user: user.data
  })
})

app.listen(port, () => console.log(`App listening on port ${port}`))
