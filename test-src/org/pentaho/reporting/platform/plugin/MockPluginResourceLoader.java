/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2010-2013 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.pentaho.platform.api.engine.IPluginResourceLoader;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by IntelliJ IDEA.
 * User: rmansoor
 * Date: 2/23/11
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */
   public class MockPluginResourceLoader implements IPluginResourceLoader {
       public MockPluginResourceLoader() {

       }
       public byte[] getResourceAsBytes(Class<? extends Object> aClass, String s) {
           throw new UnsupportedOperationException();
       }

       public String getResourceAsString(Class<? extends Object> aClass, String s) throws UnsupportedEncodingException {
           throw new UnsupportedOperationException();
       }

       public String getResourceAsString(Class<? extends Object> aClass, String s, String s1) throws UnsupportedEncodingException {
           throw new UnsupportedOperationException();
       }

       public InputStream getResourceAsStream(Class<?> aClass, String s) {
           throw new UnsupportedOperationException();
       }

       public InputStream getResourceAsStream(ClassLoader classLoader, String s) {
           try  {
                 return new FileInputStream("resource/solution/system/reporting" + s);
           } catch (FileNotFoundException fnf) {
               return null;
           }
       }

       public List<URL> findResources(Class<?> aClass, String s) {
           throw new UnsupportedOperationException();
       }

       public List<URL> findResources(ClassLoader classLoader, String s) {
            throw new UnsupportedOperationException();
       }

       public ResourceBundle getResourceBundle(Class<?> aClass, String s) {
           throw new UnsupportedOperationException();
       }

       public String getPluginSetting(Class<?> aClass, String s) {
           throw new UnsupportedOperationException();
       }

       public String getPluginSetting(Class<?> aClass, String s, String s1) {
           throw new UnsupportedOperationException();
       }

       public String getPluginSetting(ClassLoader classLoader, String s, String s1) {
           throw new UnsupportedOperationException();
       }


   }