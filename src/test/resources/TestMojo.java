import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author roland
 * @since 23/05/16
 */
@Mojo(name = "test")
public class TestMojo extends MyBaseMojo {

    /**
     * The name of the test
     */
    @Parameter
    private String name;

    // Type of the test
    @Parameter
    private String type;

    /**
     * A nested bean
     *
     * <ul>
     *     <li>Wrong item
     * </ul>
     */
    @Parameter
    private Nested nested;

    /**
     * List of nested beans
     *
     */
    @Parameter
    private List<Nested> nestedList;

    /**
     * Array of strings
     */
    @Parameter
    private String[] stringArray;

    /**
     * A map
     */
    @Parameter
    private Map<String, String> map;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
    }
}
