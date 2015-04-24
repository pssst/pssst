/**
 * Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>
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
 */
var DENY = '!?(^name$)';
var LIMIT = 536870912; // 512 MB

/**
 * Returns a new user.
 *
 * @param {String} the key (PEM format)
 * @return {Object} the new user
 */
exports.create = function create(key) {
  return {
    key: key,
    box: {
      box: [] // Default box
    }
  };
};

/**
 * Deletes an user.
 *
 * @param {Object} the user
 */
exports.erase = function erase(user) {
  user.key = null;
  user.box = null;
};

/**
 * Returns if the user is deleted.
 *
 * @param {Object} the user
 * @return {Boolean} true if deleted
 */
exports.isDeleted = function isDeleted(user) {
  return (user.key === null);
};

/**
 * Returns if the user name is denied.
 *
 * @param {String} the user name
 * @param {String} the regular expression
 * @return {Boolean} true if denied
 */
exports.isDenied = function isDenied(name, deny) {
  return new RegExp(deny || DENY).test(name);
};

/**
 * Returns if the user has reached his limit.
 *
 * @param {Object} the user
 * @param {Integer} the maximum bytes
 * @return {Boolean} true if limited
 */
exports.isLimited = function isLimited(user, limit) {
  return (JSON.stringify(user).length >= (limit || LIMIT));
};
