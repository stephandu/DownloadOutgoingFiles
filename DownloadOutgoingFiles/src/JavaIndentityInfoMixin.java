

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator=ObjectIdGenerators.UUIDGenerator.class, property="_uuid")
public class JavaIndentityInfoMixin {
  
}
