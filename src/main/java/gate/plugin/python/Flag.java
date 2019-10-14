/* 
 * Copyright (C) 2014-2016 The University of Sheffield.
 *
 * This file is part of gateplugin-Java
 * (see https://github.com/johann-petrak/gateplugin-Java)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package gate.plugin.python;

/**
 * Flag is like a mutable Boolean. It can be used as a sharable object
 * to signal a state between all duplication copies of a script.
 * @author Johann Petrak
 */
public class Flag {
  protected boolean value = false;
  public Flag(boolean value) { this.value = value; }
  public void set(boolean value) { this.value = value; }
  public boolean get() {return value; }
}
