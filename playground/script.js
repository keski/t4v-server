const API_PREFIX = 'https://ontodeside.ida.liu.se/api';

window.onload = () => {
    const target = document.getElementById("target");
    const data = document.getElementById("data");
    const owl2shacl = document.getElementById("owl2shacl");

    target.value = "http://example.org/A";
    data.value = `
@base <http://example.org/> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
<http://example.com/ontology> a owl:Ontology ;
    owl:imports <https://w3id.org/CEON/ontology/actor/0.2/> .
<A> a <Vowel> .
<Vowel> rdfs:subClassOf <Letter> .
    `.trim();
    owl2shacl.style.display = "block";
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
    const url = `${API_PREFIX}/owl2shacl?url=`;
    const ontologyUrl = document.getElementById("url").value;
    const encoded = document.getElementById("encoded");
    encoded.value = url + encodeURIComponent(ontologyUrl);
}

function getShapes() {
    const url = `${API_PREFIX}/owl2shacl?url=`;
    const ontologyUrl = document.getElementById("url").value;
    const encodedUrl = url + encodeURIComponent(ontologyUrl);

    fetch(encodedUrl)
        .then(response => response.json())
        .then(result => {
            document.getElementById("result1").value = result["result"];
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

function getTypes() {
    const target = document.getElementById("target").value;
    const data = document.getElementById("data").value;
    const schema = document.getElementById("schema").value;
    const url = `${API_PREFIX}/types`;

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
    .then(sortedData => {
        document.getElementById("result2").value = sortedData.join("\n");
    })
    .catch(error => {
        console.error('Error:', error);
    });
}

function openTab(evt, tabName) {
    const tabContent = document.getElementsByClassName("tabcontent");
    const tabLinks = document.getElementsByClassName("tablinks");

    for (let i = 0; i < tabContent.length; i++) {
        tabContent[i].style.display = "none";
    }

    for (let i = 0; i < tabLinks.length; i++) {
        tabLinks[i].classList.remove("active");
    }

    document.getElementById(tabName).style.display = "block";
    evt.currentTarget.classList.add("active");
}
