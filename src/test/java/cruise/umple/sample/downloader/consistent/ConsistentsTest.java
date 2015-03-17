package cruise.umple.sample.downloader.consistent;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.jayway.jsonassert.JsonAssert;
import com.jayway.jsonassert.JsonAsserter;

import cruise.umple.sample.downloader.Repository;
import cruise.umple.sample.downloader.repositories.TestRepository;
import cruise.umple.sample.downloader.util.MockDocumentFactoryModule;

@Guice(modules={MockDocumentFactoryModule.class})
@Test
public class ConsistentsTest {
  
  private final ConsistentsFactory factory;
  
  private ConsistentsBuilder bld;
  private final Set<Repository> repos;
  
  @Inject
  public ConsistentsTest(ConsistentsFactory factory, Set<Repository> repos) {
    this.factory = factory;
    
    this.repos = repos;
  }
  
  @BeforeMethod
  public void setup() {
    bld = factory.create(".");
  }
 
  @Test
  public void toJsonNoRepositories() throws JsonParseException, JsonMappingException, IOException {
    
    final ImportRepositorySet fromBld = bld.getRepositorySet();
    final String json = Consistents.toJson(fromBld);
    
    JsonAssert.with(json)
      .assertEquals("$.date", fromBld.getDate().getTime()).and()
      .assertEquals("$.time", fromBld.getTime().getTime()).and()   
      .assertEquals("$.rootPath", fromBld.getRootPath()).and()
      .assertEquals("$.repositories", Collections.<ImportRepository>emptyList());
  }
  
  @Test
  public void toJsonImportRepository() {
    repos.forEach(bld::withRepository);
    
    final ImportRepositorySet fromBld = bld.getRepositorySet();
    final String json = Consistents.toJson(fromBld);
    
    JsonAssert.with(json)
      .assertEquals("$.repositories[0].path", TestRepository.TEST_NAME).and()
      .assertEquals("$.repositories[0].files", Collections.<ImportFile>emptyList()); 
  }
  
  @Test
  public void toJsonImportFile() {
    repos.forEach(r -> {
      final ConsistentRepositoryBuilder rbld = bld.withRepository(r);
      r.getImports().forEach(e -> {
        try {
          e.get();
          rbld.addSuccessFile(e.getPath().toString(), e.getImportType());
        } catch (Exception ex) {
          rbld.addFailedFile(e.getPath().toString(), e.getImportType(), Throwables.getStackTraceAsString(ex));
        }
      });
    });
    
    final ImportRepositorySet fromBld = bld.getRepositorySet();
    final String json = Consistents.toJson(fromBld);
    
    final String START = "$.repositories[0].files[%d]";
    final JsonAsserter jassert = JsonAssert.with(json);
    fromBld.getRepositories().forEach(r -> {
      
      for (int i = 0; i < r.getFiles().size(); ++i) {
        final ImportFile file = r.getFile(i);
        jassert.assertEquals(String.format(START, i) + ".path", file.getPath()).and()
          .assertEquals(String.format(START, i) + ".type", file.getImportType().getName()).and()
          .assertEquals(String.format(START, i) + ".successful", file.isSuccessful()).and()
          .assertEquals(String.format(START, i) + ".message", file.getMessage());
      }
    });
    
  }
  
}
