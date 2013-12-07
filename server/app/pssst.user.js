/**
 * Pssst!
 * Copyright (C) 2013  Christian & Christian  <pssst@pssst.name>
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
 *
 * Data structure workings of an user object.
 */

/**
 * Creates an new user.
 *
 * @param {String} key in PEM format
 * @return {Object} the new user
 */
exports.create = function create(key) {
  return {
    key: key,
    box: {
      box: [] // default box
    }
  };
};

/**
 * Disables an user.
 *
 * @param {Object} the user
 */
exports.disable = function disable(user) {
  user.key = null;
  user.box = null;
};

/**
 * Checks if the user name is blocked.
 *
 * @param {String} the user name
 * @return {Boolean} true if blocked
 */
exports.isBlocked = function isBlocked(name) {
  return new RegExp('!?(^name$)').test(name);
};

/**
 * Checks if the user is disabled.
 *
 * @param {Object} the user
 * @return {Boolean} true if disabled
 */
exports.isDisabled = function isDisabled(user) {
  return (user.key === null);
};
