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
var ALLOW = '.*';
var LIMIT = 536870912; // 512 MB

/**
 * Returns a new user.
 *
 * @param {String} the key (PEM format)
 * @param {Integer} the maximum bytes
 * @return {Object} the new user
 */
exports.create = function create(key, max) {
  return {
    key: key,
    max: max || LIMIT,
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
  user.max = null;
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
 * Returns if the user name is allowed.
 *
 * @param {String} the user name
 * @param {String} the regular expression
 * @return {Boolean} true if denied
 */
exports.isDenied = function isDenied(name, allow) {
  return !(new RegExp(allow || ALLOW).test(name));
};

/**
 * Returns if the user has reached his limit.
 *
 * @param {Object} the user
 * @return {Boolean} true if limited
 */
exports.isMaximum = function isMaximum(user) {
  return (JSON.stringify(user).length >= user.max);
};
