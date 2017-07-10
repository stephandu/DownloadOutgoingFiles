

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import oracle.toplink.indirection.ValueHolder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.core.type.TypeReference;


/**
 * @JacksonBuilder is a wrapper for Jackson ObjectMapper and configuration to serialize / deserialize json
 * It is introduced boz of below factors
 * 1.high performance compare with other json framework.
 * 2.flexible configuration
 * 3.can cater circular reference well
 * 4.easy customization
 * 5.can cater the object which implement interface (which Jsonlib can cater it. e.g. Shipment -> route info is missing when using jsonlib to serialize)
 *
 */
public class JacksonBuilder{

  private static final String ES_DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private ObjectMapper objectMapper;
  private List<String> typeNames ;
//  private List<String> targetClassNameofTypeResolver = Arrays.asList("oracle.toplink.indirection.ValueHolderInterface", "java.lang.Object","com.cargosmart.");
  private List<Class> typeClasses ;
  
  private boolean enableCircleReferenceHandler = false;
  private CustomerSerializeContainFilter serializeContainFilter;

  public JacksonBuilder() {
    objectMapper = new ObjectMapper();
//    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
//    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
//    objectMapper.setVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY);
//    objectMapper.setVisibility(PropertyAccessor.SETTER, Visibility.PUBLIC_ONLY);
//    objectMapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    DateFormat dateFormat = new SimpleDateFormat(ES_DEFAULT_DATETIME_FORMAT);
    objectMapper.setDateFormat(dateFormat);
    serializeContainFilter = new CustomerSerializeContainFilter();
    typeClasses = new ArrayList<Class>();
    typeNames = new ArrayList<String>();
  }

  public ArrayList<TestObject2> getObjects(String jsonString) {
	    try {
	      return objectMapper.readValue(jsonString, new TypeReference<ArrayList<TestObject2>>() {
	      });
	    } catch (Exception e) {
	      return new ArrayList<TestObject2>();
	    }
	  }

  
  /**
   * this API is for circular reference, you can explicit set which class is the parent type here, to reduce the size of json string
   * @param javaIndentifyClass
   * @return
   */
  public JacksonBuilder setCircleReferenceClass(List<Class> javaIndentifyClass) {
    if (javaIndentifyClass != null) {
      for (Class identifyClass : javaIndentifyClass) {
        objectMapper.addMixInAnnotations(identifyClass, JavaIndentityInfoMixin.class);
      }
    }
    return this;
  }
  
  public void enableCircleReferenceHandler(){
    enableCircleReferenceHandler = true;
  }

  /**
   * the API can let you to add special annotation without inject target class by MixIn
   * @param targetClass
   * @param mixinClass
   * @return
   */
  public JacksonBuilder addMixinAnnocation(Class targetClass, Class mixinClass) {
    objectMapper.addMixInAnnotations(targetClass, mixinClass);
    return this;
  }

  public JacksonBuilder setDateFormat(String dateFormatString) {
    DateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    objectMapper.setDateFormat(dateFormat);
    return this;
  }

  public JacksonBuilder setIncludes(String[] includes) {
    serializeContainFilter.addInclude(includes);
    return this;
  }

  public JacksonBuilder setExcludes(String[] excludes) {
    serializeContainFilter.addExclude(excludes);
    return this;
  }

  public JacksonBuilder setExcludesByClass(Class target, String[] excludes) {
    serializeContainFilter.addExcludeByClassField(target, excludes);
    return this;
  }

  public JacksonBuilder setAllFiledsWithGetter(boolean allFiledsWithGetter) {
    throw new RuntimeException(new UnsupportedOperationException("setAllFiledsWithGetter is unsupported in JacksonBuilder currently"));
  }

  public JacksonBuilder setIgnoreReadOnlyFields(boolean ignoreReadOnlyFields) {
    throw new RuntimeException(new UnsupportedOperationException("setIgnoreReadOnlyFields is unsupported in JacksonBuilder currently"));
  }

  public JacksonBuilder setExcludeTypes(Class[] excludeTypes) {
    serializeContainFilter.addExcludeByType(excludeTypes);
    return this;
  }


  public <T extends Serializable> String toJsonString(T object) {
    prepareObjectMapper();

    try {
      return objectMapper.writeValueAsString(object);

    } catch (JsonProcessingException e) {
      if (e.getCause() instanceof StackOverflowError) {
        throw new RuntimeException("Circular reference found without setJavaIndentifyClass, please set it by API setJavaIndentifyClass", e);
      } else {
        throw new RuntimeException(e);
      }

    }

  }

  public <T extends Serializable> T fromJsonString(String jsonString, Class<T> clazz) {
    return this.fromJsonString(jsonString, clazz, true);
  }

  public <T extends Serializable> T fromJsonString(String jsonString, Class<T> clazz, boolean enableValueHolder) {
    prepareObjectMapper();
    T object = null;
    try {
      object = objectMapper.readValue(jsonString, clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return object;
  }
  
  public ArrayList<OutgoingSolrLeg> getSolrLegInfo(String jsonString) {
    try {
      return objectMapper.readValue(jsonString, new TypeReference<ArrayList<OutgoingSolrLeg>>() {
      });
    } catch (Exception e) {
      return new ArrayList<OutgoingSolrLeg>();
    }
  }

  private void prepareObjectMapper() {
    
//    if(CollectionUtils.isNotEmpty(typeClasses) || CollectionUtils.isNotEmpty(typeNames)){
//      CustomerTypeResolverBuilder typeResolver = new CustomerTypeResolverBuilder(typeNames,typeClasses);
//      typeResolver.init(Id.CLASS, null);
//      typeResolver.inclusion(As.PROPERTY);
//      typeResolver.typeProperty("_class");
//      objectMapper.setDefaultTyping(typeResolver);
//    }
//    

    SimpleModule valueHolderModule = new SimpleModule("valueHolderModule", new Version(1, 0, 0, null, null, null));
    valueHolderModule.addDeserializer(ValueHolder.class, new ValueHolderDeserializer(FullCustomzerMixIn.class));
    objectMapper.registerModule(valueHolderModule);
    

    if (!serializeContainFilter.isEmptyFilter()) {
      SimpleFilterProvider simpleFilterProvider = new SimpleFilterProvider();
      simpleFilterProvider.addFilter("jsonFilter", serializeContainFilter);
      objectMapper.setFilters(simpleFilterProvider);
      objectMapper.addMixInAnnotations(Object.class,PropertyFilterMixIn.class);
    }
    
    if(enableCircleReferenceHandler){
      objectMapper.addMixInAnnotations(Object.class, FullCustomzerMixIn.class);
    }
    //exclude null and empty fields
    objectMapper.setSerializationInclusion(Include.NON_EMPTY);

  }

  
  public void addTypeIntoJsonFor(Class targetClass){
    typeClasses.add(targetClass);
  }
  
  public void addTypeIntoJsonFor(List<Class> targetClass){
    typeClasses.addAll(targetClass);
  }
  
  public void addTypeIntoForJson(String className){
    typeNames.add(className);
  }
  
  public void addTypeIntoForJson(List<String> classNames){
    typeNames.addAll(classNames);
  }

}
