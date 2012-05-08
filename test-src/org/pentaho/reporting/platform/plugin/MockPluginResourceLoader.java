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