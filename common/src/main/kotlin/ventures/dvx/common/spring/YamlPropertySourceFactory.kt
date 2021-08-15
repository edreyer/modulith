package ventures.dvx.common.spring

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory

class YamlPropertySourceFactory : PropertySourceFactory {
  override fun createPropertySource(name: String?, resource: EncodedResource): PropertySource<*> {
    val factory = YamlPropertiesFactoryBean()
    factory.setResources(resource.resource)
    val properties = factory.`object`!!
    return PropertiesPropertySource(resource.resource.filename!!, properties)
  }
}
