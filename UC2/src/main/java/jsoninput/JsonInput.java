
package jsoninput;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "PublicList"
})
public class JsonInput {

    @JsonProperty("PublicList")
    private List<jsoninput.PublicList> PublicList = new ArrayList<jsoninput.PublicList>();

    /**
     * 
     * @return
     *     The PublicList
     */
    @JsonProperty("PublicList")
    public List<jsoninput.PublicList> getPublicList() {
        return PublicList;
    }

    /**
     * 
     * @param PublicList
     *     The PublicList
     */
    @JsonProperty("PublicList")
    public void setPublicList(List<jsoninput.PublicList> PublicList) {
        this.PublicList = PublicList;
    }

}
