package l1j.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

class Java21CompatibilityTest {
    @Test
    void bossCycleXmlCanBeReadWithExternalJaxb() throws Exception {
        assertXmlCanBeRead("l1j.server.server.model.L1BossCycle$L1BossCycleList",
                "data/xml/Cycle/BossCycle.xml");
    }

    @Test
    void treasureBoxXmlCanBeReadWithExternalJaxb() throws Exception {
        assertXmlCanBeRead("l1j.server.server.model.item.L1TreasureBox$TreasureBoxList",
                "data/xml/Item/TreasureBox.xml");
    }

    @Test
    void startupResourcesAndDependenciesAreAvailable() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("config/server.properties")));
        assertNotNull(getClass().getResource("/c3p0-config.xml"));
        assertNotNull(Class.forName("com.mysql.jdbc.Driver"));
        assertNotNull(Class.forName("com.mchange.v2.c3p0.ComboPooledDataSource"));
        assertNotNull(Class.forName("javolution.util.FastMap"));
        assertNotNull(Class.forName("org.apache.tools.zip.ZipFile"));
    }

    private void assertXmlCanBeRead(String className, String fileName) throws Exception {
        Class<?> rootClass = Class.forName(className);
        Object value = JAXBContext.newInstance(rootClass).createUnmarshaller()
                .unmarshal(Path.of(fileName).toFile());
        assertEquals(rootClass, value.getClass());
    }
}
