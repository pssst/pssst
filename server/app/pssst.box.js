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
 * Creates a new box.
 *
 * @param {Object} the user
 * @param {String} the box name
 */
exports.create = function create(user, box) {
  user.box[box] = [];
};

/**
 * Deletes a box.
 *
 * @param {Object} the user
 * @param {String} the box name
 */
exports.erase = function erase(user, box) {
  delete user.box[box];
};

/**
 * Returns a list of all user boxes.
 *
 * @param {Object} the user
 * @return {Object} list of box names
 */
exports.list = function list(user) {
  return Object.keys(user.box).sort();
};

/**
 * Returns the box object.
 *
 * @param {Object} the user
 * @param {String} the box name
 * @return {Object} the box or null
 */
exports.find = function find(user, box) {
  if (box in user.box) {
    return {
      /**
       * The associated user.
       *
       * @type {Object} the user
       */
      user: user,

      /**
       * Pulls the first message from the box.
       *
       * @return {Object} the message
       */
      pull: function pull() {
        return user.box[box].shift();
      },

      /**
       * Pushes a message into the box.
       *
       * @param {Object} the message
       */
      push: function push(message) {
        user.box[box].push(message);
      }
    };
  } else {
    return null; // Box not found
  }
};

/**
 * Returns if the box name is denied.
 *
 * @param {String} the box name
 * @return {Boolean} true if denied
 */
exports.isDenied = function isDenied(box) {
  return new RegExp('^(box)|(key)|(list)$').test(box);
};
