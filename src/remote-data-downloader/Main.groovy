import remote.RemoteSystem
import local.Configuration
import local.DataDownloader

def config = new Configuration()
config.remoteSystem = new RemoteSystem()
config.allowedItemTypes = [
		'CONTAINER_1',
		'CONTAINER_2',
		'CONTAINER_3',
		'ELEMENT_1',
		'ELEMENT_2',
		'ELEMENT_3',
	]
config.outputDirectory = './work'

new DataDownloader(config).fetch('CONTAINER_1', 'Container1')
