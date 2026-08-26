package com.joyhsin.hotspot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class SceneStore {
  private final ObjectMapper mapper; private final Path file;
  public SceneStore(ObjectMapper mapper,AppProperties props) throws IOException { this.mapper=mapper; file=props.dataDir().toAbsolutePath().normalize().resolve("scenes.json"); Files.createDirectories(file.getParent()); if(Files.notExists(file)) write(List.of()); }
  public synchronized List<SceneData> read(){ try { return mapper.readValue(file.toFile(),new TypeReference<>(){}); } catch(IOException e){ throw new IllegalStateException("无法读取场景数据",e); } }
  public synchronized void write(List<SceneData> scenes){ try { Path tmp=Files.createTempFile(file.getParent(),"scenes-",".tmp"); mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(),scenes); try { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); } catch(AtomicMoveNotSupportedException e){ Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); } } catch(IOException e){ throw new IllegalStateException("无法保存场景数据",e); } }
}
