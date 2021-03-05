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
const fs = require("fs");
const jwt = require('jsonwebtoken')
const { Liquid } = require('liquidjs')
const port = 3000

const app = express()
app.use(bodyParser.json())
app.engine('liquid', new Liquid().express())
app.set('views', './views')
app.set('view engine', 'liquid')

const reauth = Axios.create({
  baseURL: 'http://localhost:6660/'
})

app.get('/', (req, res) => {
  res.sendFile('views/index.html', { root: __dirname })
})

app.get('/authorize', (req, res) => {
  res.sendFile('views/authorize.html', { root: __dirname })
})

app.get('/authorize/consent', (req, res) => {
  const token = jwt.verify(
    req.query.token,
    fs.readFileSync('./server_public_key.pem'),
    { issuer: "reauth" },
    (_, decoded) => {
      if (decoded && decoded.sub === "resource_owner_consent") {
        console.log(decoded)
        res.render('consent.liquid', decoded)
      } else {
        res.sendStatus(500)
      }
    }
  )
})

app.get('/callback', async (req, res) => {
  const body = {
    "code": req.query.code,
    "grant_type": "authorization_code",
    "redirect_uri": "http://localhost:3000",
    "client_id": "007a0bae-926f-4c9c-90d6-bbf533281f98",
    "client_secret": "26a238915b9d29ea47a1979fd540628958247de5108cc0844f707563736e60753f08470a8503b4d6ba12937cf1b56959293879043faa89ff5f1118b8163bb65112f3b7baf5b20501a711ae939f13c2549f0e9705b3482afa9536f47706f0ea065dbb521f81dbb309a7f85f555aea2d024920bdf33828961db2e8208704eee8fe"
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
