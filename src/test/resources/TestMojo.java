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
public class TestMojo extends AbstractMojo {

    /**
     * The name of the test
     */
    @Parameter
    private String name;

    // Type of the test
    @Parameter
    private String type;

    /**
     * A nested beand
     */
    @Parameter
    private Nested nested;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {}
}
