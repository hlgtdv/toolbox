import remote.RemoteSystem
import local.DataDownloader

new DataDownloader(new RemoteSystem()).fetch('CONTAINER_1', 'Container1', true)
