

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import oracle.toplink.indirection.ValueHolder;


import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.Deserializers.Base;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;

public class ValueHolderDeserializer extends JsonDeserializer<ValueHolder> {

  private Class targetClass;
  private final static String VALUEHOLDER_FIELDNAME = "value";
  
  public ValueHolderDeserializer(Class targetClass) {
    this.targetClass = targetClass;
  }
  @Override
  public ValueHolder deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
    ValueHolder valueHolder = new ValueHolder();
    JsonToken currentToken = parser.getCurrentToken();
    while(parser.getCurrentToken() != JsonToken.END_OBJECT){
      if(VALUEHOLDER_FIELDNAME.equals(parser.getText()) ){
        currentToken = parser.nextToken();
        if(currentToken == JsonToken.START_OBJECT){
          JavaType type1 = context.constructType(Object.class); 
          JsonDeserializer<Object> beanDeserilizer = context.findRootValueDeserializer(type1);
          valueHolder.setValue(beanDeserilizer.deserialize(parser, context));
          parser.nextToken();
          break;
        }
        
        String uuid = parser.getText();     
          JavaType type = context.constructType(targetClass);
          BeanDescription beanDesc = context.getConfig().introspect(type);
          ObjectIdGenerator<?> idGenerator = context.objectIdGeneratorInstance(beanDesc.getClassInfo(), beanDesc.getObjectIdInfo());
          
          final ReadableObjectId objectId = context.findObjectId(UUID.fromString(uuid), idGenerator);
          if(objectId!=null){
            valueHolder.setValue(objectId.item);
          }
        
      }
//      System.out.println(parser.getText());
      currentToken = parser.nextToken();
    }
    
//    return super.deserialize(parser, context, valueHolder);
    
    
    return valueHolder;
  }

}
