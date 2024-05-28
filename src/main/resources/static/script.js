window.onload = () => {
    document.getElementById("target").value = "http://example.org/A";
    document.getElementById("data").value =
        `@base <http://example.org/> .
    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
    @prefix owl: <http://www.w3.org/2002/07/owl#> .
    <http://example.com/ontology> a owl:Ontology ;
        owl:imports <https://w3id.org/CEON/ontology/actor/0.2/> .
    <A> a <Vowel> .
    <Vowel> rdfs:subClassOf <Letter> .`;
    
    document.getElementById("owl2shacl").style.display = "block";

}

document.getElementById("myform1").onsubmit = (event) => {
    event.preventDefault();
    document.getElementById("result1").value = "Loading...";
    getShapes();
};

document.getElementById("myform2").onsubmit = (event) => {
    event.preventDefault();
    document.getElementById("result2").value = "Loading...";
    getTypes();
};

function encode() {
    const url = '/api/owl2shacl?url=';
    let ontology_url = document.getElementById("url").value;
    let encoded = document.getElementById("encoded");
    encoded.value = url + encodeURIComponent(ontology_url);
}

function getShapes() {
    const url = '/api/owl2shacl?url=';
    let ontology_url = document.getElementById("url").value;
    let encoded = document.getElementById("encoded");
    encoded_url = url + encodeURIComponent(ontology_url);

    // Perform the GET request
    fetch(encoded_url)
        .then(response => response.json())
        .then(result => {
            document.getElementById("result1").value = result["result"];
        })
        .catch(error => {
            console.error('Error:', error);
        });

}

function getTypes() {
    let target = document.getElementById("target").value;
    let data = document.getElementById("data").value;
    let schema = document.getElementById("schema").value;

    // Define the URL and parameters
    const url = '/api/types';

    // Perform the POST request
    fetch(url, {
        method: "POST",
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ target, data, schema })
    })
        .then(response => response.json())
        .then(response => response.sort())
        .then(data => {
            document.getElementById("result2").value = data.join("\n");
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

function openTab(evt, tabName) {
    // Get all elements with class="tabcontent" and hide them
    let tabcontent = document.getElementsByClassName("tabcontent");
    for (let i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    // Get all elements with class="tablinks" and remove the class "active"
    let tablinks = document.getElementsByClassName("tablinks");
    for (let i = 0; i < tablinks.length; i++) {
        tablinks[i].classList.remove("active");
    }

    // Show the current tab, and add an "active" class to the button that opened the tab
    document.getElementById(tabName).style.display = "block";
    evt.currentTarget.classList.add("active");
} 