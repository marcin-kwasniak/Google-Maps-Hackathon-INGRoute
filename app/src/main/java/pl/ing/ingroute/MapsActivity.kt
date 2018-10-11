package pl.ing.ingroute

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.google.maps.errors.ApiException
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode
import kotlinx.android.synthetic.main.activity_maps.*
import org.joda.time.DateTime
import java.io.IOException
import java.util.concurrent.TimeUnit


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val overview = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(toolbar)
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fab.setOnClickListener { findBestRoute() }
    }

    private fun getDirectionsDetails(origin: LatLng, destination: LatLng, mode: TravelMode): DirectionsResult? {
        val now = DateTime()
        try {
            return DirectionsApi.newRequest(getGeoContext())
                    .mode(mode)
                    .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                    .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                    .departureTime(now)
                    .await()
        } catch (e: ApiException) {
            e.printStackTrace()
            return null
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        markers(googleMap)
        googleMap.setPadding(0, 0, resources.getDimensionPixelSize(R.dimen.map_zoom_buttons_right_margin),
                resources.getDimensionPixelSize(R.dimen.map_zoom_buttons_bottom_margin))
    }

    private fun markers(googleMap: GoogleMap) {
        val locationManager = LocationManager.newInstance()
        val allLocations = locationManager.getAllDepartmentsLocation()
        allLocations.forEach {
            googleMap.addMarker(MarkerOptions()
                    .position(it.position.coordinates)
                    .title(it.street))
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(allLocations[0].position.coordinates, 12f))

        val currentLocation = locationManager.getCurrentLocation(this)

        googleMap.addMarker(MarkerOptions()
                .position(currentLocation.coordinates)
                .title("Twoja lokalizacja")).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

        googleMap.setOnMarkerClickListener {
            route(googleMap, currentLocation.coordinates, it.position)
            return@setOnMarkerClickListener true
        }
    }

    private fun route(googleMap: GoogleMap, origin: LatLng, destination: LatLng) {
        setupGoogleMapScreenSettings(googleMap)
        val results = getDirectionsDetails(origin, destination, TravelMode.DRIVING)
        if (results != null) {
            addPolyline(results, googleMap)
            positionCamera(results.routes[overview], googleMap)
            addMarkersToMap(results, googleMap)
        }
    }


    private fun setupGoogleMapScreenSettings(mMap: GoogleMap) {
        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true
        mMap.isTrafficEnabled = true
        val mUiSettings = mMap.uiSettings
        mUiSettings.isZoomControlsEnabled = true
        mUiSettings.isCompassEnabled = true
        mUiSettings.isMyLocationButtonEnabled = true
        mUiSettings.isScrollGesturesEnabled = true
        mUiSettings.isZoomGesturesEnabled = true
        mUiSettings.isTiltGesturesEnabled = true
        mUiSettings.isRotateGesturesEnabled = true
    }

    private fun addMarkersToMap(results: DirectionsResult, mMap: GoogleMap) {
        mMap.addMarker(MarkerOptions().position(LatLng(results.routes[overview].legs[overview].startLocation.lat, results.routes[overview].legs[overview].startLocation.lng)).title(results.routes[overview].legs[overview].startAddress))
        mMap.addMarker(MarkerOptions().position(LatLng(results.routes[overview].legs[overview].endLocation.lat, results.routes[overview].legs[overview].endLocation.lng)).title(results.routes[overview].legs[overview].startAddress).snippet(getEndLocationTitle(results)))
    }

    private fun positionCamera(route: DirectionsRoute, mMap: GoogleMap) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(route.legs[overview].startLocation.lat, route.legs[overview].startLocation.lng), 12f))
    }

    var polyline: Polyline? = null

    private fun addPolyline(results: DirectionsResult, mMap: GoogleMap) {
        polyline?.remove()
        val decodedPath = PolyUtil.decode(results.routes[overview].overviewPolyline.encodedPath)
        polyline = mMap.addPolyline(PolylineOptions().addAll(decodedPath))
    }

    private fun getEndLocationTitle(results: DirectionsResult): String {
        return "Time :" + results.routes[overview].legs[overview].duration.humanReadable + " Distance :" + results.routes[overview].legs[overview].distance.humanReadable
    }

    private fun getGeoContext(): GeoApiContext {
        val geoApiContext = GeoApiContext()
        return geoApiContext
                .setQueryRateLimit(3)
                .setApiKey(getString(R.string.google_maps_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS)
    }

    private fun findBestRoute() {
        Toast.makeText(this, "TODO Znajdź najlepszą trasę", Toast.LENGTH_SHORT).show()
        //TODO
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
