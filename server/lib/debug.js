/**
 * Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Debug middleware. Available debug levels:
 *
 *   Request
 *
 *   0 = off
 *   1 = print request method and URL
 *   2 = print request headers also
 *   3 = print request body also
 *
 *   Response
 *
 *   0 = off
 *   1 = print response status code
 *   3 = print response body also
 *
 * @param {Number} debug level
 * @param {Object} request
 * @param {Object} response
 * @param {Object} next handler
 */
module.exports = function debug(level, req, res, next) {
  var time = new Date().getTime();

  if (level > 0) {
    console.info(time, req.method, req.url);
  }

  if (level > 1) {
    console.info(time, req.headers);
  }

  if (level > 2) {
    console.info(time, req.body);
  }

  // Monkey patching
  var end = res.end;
  res.end = function patch(chunk, encoding) {
    var time = new Date().getTime();

    if (level > 0) {
      console.info(time, res.statusCode);
    }

    if (level > 2) {
      console.info(time, chunk.toString(encoding));
    }

    res.end = end;
    res.end(chunk, encoding);
  };

  next();
}
