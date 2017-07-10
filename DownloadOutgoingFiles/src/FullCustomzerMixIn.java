

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonFilter("jsonFilter")
@JsonIdentityInfo(generator=ObjectIdGenerators.UUIDGenerator.class, property="_uuid")
public class FullCustomzerMixIn {

}
