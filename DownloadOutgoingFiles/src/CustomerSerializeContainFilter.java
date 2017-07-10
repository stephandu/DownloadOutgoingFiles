

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

public class CustomerSerializeContainFilter extends SimpleBeanPropertyFilter {

  /**
   * Set of property names to filter out.
   */
  protected final Set<String> _propertiesToExclude;
  protected final Set<String> _propertiesToInclude;
  protected final Set<Class> _classToExclude;
  protected final Map<Class,Set<String>> _classPropertiesToExclude;
  private boolean emptyFilter = true;
  

  public CustomerSerializeContainFilter() {
    _propertiesToInclude = new HashSet<String>();
    _propertiesToExclude = new HashSet<String>();
    _classToExclude = new HashSet<Class>();
    _classPropertiesToExclude = new HashMap<Class, Set<String>>();
  }
  
  public void addInclude(String[] includes){
    
    _propertiesToInclude.addAll(Arrays.asList(includes));
    emptyFilter=false;
  }
  
  /*
   * addExclude is higher priority than addExceptFilterForClass
   * */
  public void addExclude(String[] excludes){
   
    
    _propertiesToExclude.addAll(Arrays.asList(excludes));
    emptyFilter=false;
  }
  
  public void addExcludeByClassField(Class clazz, String[] excludes){
    
    if(_classPropertiesToExclude.containsKey(clazz)){
      _classPropertiesToExclude.get(clazz).addAll(Arrays.asList(excludes));
    }else{
      _classPropertiesToExclude.put(clazz, new HashSet(Arrays.asList(excludes)));
    }
    emptyFilter=false;
    
  }
  
  public void addExcludeByType(Class[] clazz){
    if(clazz != null){
      _classToExclude.addAll(Arrays.asList(clazz));
    }
    emptyFilter=false;
  }
  
  @Override
  protected boolean include(BeanPropertyWriter writer) {
    return include(null,writer);
  }
 
  protected boolean include(Object pojo,BeanPropertyWriter writer) {
    String fieldName = writer.getName();
    try {
      String capitalizeFieldName = StringUtils.capitalize(fieldName);
      Method readMethod = pojo.getClass().getMethod("get"+capitalizeFieldName);
      pojo.getClass().getMethod("set"+capitalizeFieldName, readMethod.getReturnType());
    } catch (NoSuchMethodException e) {
      return false;
    } 

    if(_propertiesToExclude.contains(fieldName)){
      return false;
    }
    final AnnotatedMember member = writer.getMember();
    
    for (Class excludeClass : _classToExclude) {
      if(excludeClass.isAssignableFrom(member.getRawType())){
        return false;
      }
    }
    if(pojo!=null && _classPropertiesToExclude.containsKey(pojo.getClass()) && _classPropertiesToExclude.get(pojo.getClass()).contains(fieldName)){
      return false;
    }

   
    return _propertiesToInclude.contains(fieldName);
  }
 
  @Override
  public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider provider, BeanPropertyWriter writer) throws Exception {
    if (include(bean,writer))
      writer.serializeAsField(bean, jgen, provider);
  }
  
  public boolean isEmptyFilter() {
    return emptyFilter;
  }

}
