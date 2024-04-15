import local.Configuration
import local.RepositoryDocumentator

def config = new Configuration()
config.configDirectory = './config/documentator'
config.inputDirectory = './input'
config.outputDirectory = './output'

new RepositoryDocumentator(config).run()
